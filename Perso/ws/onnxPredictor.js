// onnxPredictor.js
class OnnxPersonalityPredictor {
    constructor(modelPath = './modello_personalita.onnx') {
        this.modelPath = modelPath;
        this.session = null;
        this.isModelLoaded = false;
        // Default class labels, will be updated if model provides them
        this.classLabels = ["Extrovert", "Introvert", "Ambivert"];
        this.loadOnnxModel();
    }

    async loadOnnxModel() {
        try {
            console.log(`[ONNX] Attempting to load model from: ${this.modelPath}`);
            this.session = await ort.InferenceSession.create(this.modelPath);
            this.isModelLoaded = true;
            console.log('[ONNX] Model loaded successfully.');

            // Attempt to get class labels from model metadata if available
            // This is highly dependent on how the ONNX model was exported
            // Some models might store class labels in metadata or as a dedicated output.
            // For now, we'll rely on our predefined labels or a dedicated output if present.
            // In many cases, ONNX models from frameworks like scikit-learn will have numerical outputs
            // and the mapping to labels is done externally.
            console.log("Model Input Names:", this.session.inputNames);
            console.log("Model Output Names:", this.session.outputNames);

            // If your ONNX model has a specific output for class labels (e.g., "class_labels_output"),
            // you might fetch it here. For simplicity, we'll use the hardcoded ones
            // unless the actual prediction output implies a different order.
            // The python script for ONNX export should be designed to match these.

        } catch (e) {
            console.error(`[ONNX ERROR] Failed to load ONNX model: ${e.message}`);
            console.error(e);
            this.isModelLoaded = false;
            // Potentially alert the user or disable prediction features
            throw new Error("Impossibile caricare il modello di previsione. Riprova più tardi.");
        }
    }

    /**
     * Extracts features from the questionnaire response into a Float32Array.
     * The order and type of features must match the ONNX model's expected input.
     *
     * @param {Object} response - The QuestionnaireResponse object.
     * @returns {Float32Array} An array of features.
     */
    extractFeatures(response) {
        // Ensure the order matches the training data features
        // (oreTrascorseDaSolo, pauraDelPalcoscenico, partecipazioneEventiSociali,
        // usciteSettimanali, stanchezzaDopoSocializzazione, numeroAmiciStretti, frequenzaPostSocial)
        const features = [
            response.oreTrascorseDaSolo,
            response.pauraDelPalcoscenico ? 1.0 : 0.0,
            response.partecipazioneEventiSociali,
            response.usciteSettimanali,
            response.stanchezzaDopoSocializzazione ? 1.0 : 0.0,
            response.numeroAmiciStretti,
            response.frequenzaPostSocial
        ];
        console.log("Extracted Features:", features);
        return new Float32Array(features);
    }

    /**
     * Predicts personality type based on questionnaire responses.
     * @param {Object} response - The questionnaire responses.
     * @returns {Promise<{prediction: string, probabilities: Map<string, number>}>}
     */
    async predict(response) {
        if (!this.isModelLoaded || !this.session) {
            throw new Error("Il modello ONNX non è stato caricato correttamente.");
        }

        try {
            const inputFeatures = this.extractFeatures(response);

            // Assuming your model has one input, get its name
            const inputName = this.session.inputNames[0];
            const dims = [1, inputFeatures.length]; // Batch size 1, 7 features

            const inputTensor = new ort.Tensor('float32', inputFeatures, dims);

            const feeds = { [inputName]: inputTensor };
            console.log("Input feeds:", feeds);

            const results = await this.session.run(feeds);
            console.log("ONNX Prediction Results:", results);

            // Based on the Python PersonalityPredictor, the outputs are:
            // 0: label (e.g., 'Extrovert')
            // 1: probabilities (e.g., [0.1, 0.8, 0.1])
            // 2: class_labels (e.g., ['Extrovert', 'Introvert', 'Ambivert']) - often string tensor

            // Extract predicted label
            let predictedLabelTensor;
            if (this.session.outputNames.includes('label')) { // Check by specific name first
                predictedLabelTensor = results['label'];
            } else if (results[this.session.outputNames[0]]) { // Fallback to first output
                predictedLabelTensor = results[this.session.outputNames[0]];
            } else {
                 throw new Error("Output 'label' non trovato o primo output non disponibile.");
            }
            
            let predictedClass = 'Unknown';
            if (predictedLabelTensor) {
                // ONNX Runtime Web might return a string tensor as a string array or a single string
                if (predictedLabelTensor.data && predictedLabelTensor.data.length > 0) {
                    predictedClass = predictedLabelTensor.data[0];
                } else if (typeof predictedLabelTensor.data === 'string') {
                    predictedClass = predictedLabelTensor.data;
                }
            }
            console.log("Predicted Class Raw:", predictedLabelTensor?.data, "->", predictedClass);


            // Extract probabilities
            let probabilitiesTensor;
            if (this.session.outputNames.includes('probabilities')) { // Check by specific name
                probabilitiesTensor = results['probabilities'];
            } else if (results[this.session.outputNames[1]]) { // Fallback to second output
                 probabilitiesTensor = results[this.session.outputNames[1]];
            } else {
                throw new Error("Output 'probabilities' non trovato o secondo output non disponibile.");
            }

            let probabilitiesArray = [];
            if (probabilitiesTensor && probabilitiesTensor.data) {
                // probabilitiesTensor.data is typically a Float32Array
                probabilitiesArray = Array.from(probabilitiesTensor.data);
            }
            console.log("Probabilities Raw:", probabilitiesTensor?.data, "->", probabilitiesArray);

            // Update classLabels if provided by the model (e.g., as a third output)
            let modelClassLabelsTensor;
            if (this.session.outputNames.includes('class_labels')) {
                modelClassLabelsTensor = results['class_labels'];
            } else if (results[this.session.outputNames[2]]) { // Fallback to third output
                modelClassLabelsTensor = results[this.session.outputNames[2]];
            }

            if (modelClassLabelsTensor && modelClassLabelsTensor.data && modelClassLabelsTensor.data.length > 0) {
                this.classLabels = Array.from(modelClassLabelsTensor.data);
                console.log("Class Labels updated from model:", this.classLabels);
            }


            // Create the probability map
            const probMap = new Map();
            if (probabilitiesArray.length === this.classLabels.length) {
                this.classLabels.forEach((label, index) => {
                    probMap.set(label, probabilitiesArray[index]);
                });
            } else {
                console.warn("[ONNX] Mismatch between probability array length and class labels length. Attempting best fit.");
                // Fallback for mismatch - assign probabilities by index if names don't match
                const minLength = Math.min(probabilitiesArray.length, this.classLabels.length);
                for (let i = 0; i < minLength; i++) {
                    probMap.set(this.classLabels[i], probabilitiesArray[i]);
                }
                // If more class labels than probabilities, fill with 0
                for (let i = minLength; i < this.classLabels.length; i++) {
                    probMap.set(this.classLabels[i], 0.0);
                }
                // If more probabilities than class labels, add generic ones
                for (let i = minLength; i < probabilitiesArray.length; i++) {
                    probMap.set(`Class_${i}`, probabilitiesArray[i]);
                }
            }

            return { prediction: predictedClass, probabilities: probMap };

        } catch (e) {
            console.error(`[ONNX ERROR] Prediction failed: ${e.message}`);
            console.error(e);
            throw new Error("Errore durante la previsione della personalità. Riprova.");
        }
    }
}
