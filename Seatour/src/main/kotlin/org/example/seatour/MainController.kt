package org.example.seatour

// MainController.kt
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.geometry.Insets
import javafx.geometry.Pos
import java.time.LocalDate
import java.time.LocalTime

class MainController {
    private val databaseManager = DatabaseManager()
    private var livelloSelezionatoAndata: Int = 0
    private var livelloSelezionatoRitorno: Int = 0
    private val pulsantiAndataAffollamento = mutableListOf<Button>()
    private val pulsantiRitornoAffollamento = mutableListOf<Button>()

    fun creaInterfaccia(): VBox {
        val root = VBox(20.0)
        root.padding = Insets(20.0)
        root.alignment = Pos.CENTER

        // Titolo
        val titolo = Label("Annotazione Affollamento Bus")
        titolo.style = "-fx-font-size: 24px; -fx-font-weight: bold;"

        // Sezione Andata
        val titoloAndata = Label("ANDATA")
        titoloAndata.style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2196F3;"
        val grigliaPulsantiAndata = creaPulsantiAffollamento("ANDATA")

        // Sezione Ritorno
        val titoloRitorno = Label("RITORNO")
        titoloRitorno.style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FF5722;"
        val grigliaPulsantiRitorno = creaPulsantiAffollamento("RITORNO")

        // Pulsanti azione
        val pulsanteConferma = Button("Conferma Annotazioni")
        pulsanteConferma.style = "-fx-font-size: 16px; -fx-padding: 10px 20px;"
        pulsanteConferma.setOnAction { confermaAnnotazioni() }

        val pulsanteReport = Button("Visualizza Report")
        pulsanteReport.style = "-fx-font-size: 16px; -fx-padding: 10px 20px;"
        pulsanteReport.setOnAction { mostraReport() }

        val boxPulsanti = HBox(20.0)
        boxPulsanti.alignment = Pos.CENTER
        boxPulsanti.children.addAll(pulsanteConferma, pulsanteReport)

        // Status
        // val statusLabel = Label("Seleziona il livello di affollamento per andata e/o ritorno (1=minimo, 5=massimo)")
        // statusLabel.style = "-fx-font-size: 14px; -fx-text-fill: #666666;"

        root.children.addAll(
            titolo,
            titoloAndata, grigliaPulsantiAndata,
            titoloRitorno, grigliaPulsantiRitorno,
            boxPulsanti,
            // statusLabel
        )
        return root
    }

    private fun creaPulsantiAffollamento(direzione: String): GridPane {
        val griglia = GridPane()
        griglia.hgap = 10.0
        griglia.vgap = 10.0
        griglia.alignment = Pos.CENTER

        val colori = arrayOf(
            "#4CAF50", // Verde
            "#8BC34A", // Verde chiaro
            "#FFC107", // Giallo
            "#FF9800", // Arancione
            "#F44336"  // Rosso
        )

        // Una riga di 5 pulsanti per ogni direzione
        for (i in 1..5) {
            val pulsante = Button(i.toString())
            pulsante.prefWidth = 80.0
            pulsante.prefHeight = 60.0
            pulsante.style = "-fx-background-color: ${colori[i-1]}; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"

            pulsante.setOnAction { selezionaLivello(i, direzione) }

            if (direzione == "ANDATA") {
                pulsantiAndataAffollamento.add(pulsante)
            } else {
                pulsantiRitornoAffollamento.add(pulsante)
            }

            griglia.add(pulsante, i-1, 0)
        }

        return griglia
    }

    private fun selezionaLivello(livello: Int, direzione: String) {
        if (direzione == "ANDATA") {
            livelloSelezionatoAndata = livello
            aggiornaStilePulsanti(pulsantiAndataAffollamento, livello)
        } else {
            livelloSelezionatoRitorno = livello
            aggiornaStilePulsanti(pulsantiRitornoAffollamento, livello)
        }
    }

    private fun aggiornaStilePulsanti(pulsanti: List<Button>, livelloSelezionato: Int) {
        // Reset stili pulsanti
        pulsanti.forEach { pulsante ->
            val originalColor = when (pulsante.text) {
                "1" -> "#4CAF50"
                "2" -> "#8BC34A"
                "3" -> "#FFC107"
                "4" -> "#FF9800"
                "5" -> "#F44336"
                else -> "#CCCCCC"
            }
            pulsante.style = "-fx-background-color: $originalColor; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"
        }

        // Evidenzia pulsante selezionato
        pulsanti.filter { it.text == livelloSelezionato.toString() }.forEach { pulsante ->
            pulsante.style += "; -fx-border-color: black; -fx-border-width: 3px;"
        }
    }

    private fun confermaAnnotazioni() {
        // Verifica che entrambe le direzioni siano selezionate
        if (livelloSelezionatoAndata == 0 || livelloSelezionatoRitorno == 0) {
            mostraAlert("Errore", "Devi selezionare il livello di affollamento per ENTRAMBE le direzioni (andata E ritorno).")
            return
        }

        var annotazioniSalvate = 0
        val messaggiErrore = mutableListOf<String>()

        // Controlla se esistono già annotazioni per oggi
        if (databaseManager.haAnnotazioneOggi("ANDATA") || databaseManager.haAnnotazioneOggi("RITORNO")) {
            mostraAlert("Errore", "Hai già inserito le annotazioni per oggi!")
            return
        }

        // Salva andata
        val annotazioneAndata = AnnotazioneAffollamento(
            data = LocalDate.now(),
            orario = LocalTime.now(),
            livelloAffollamento = livelloSelezionatoAndata,
            direzione = "ANDATA"
        )

        if (databaseManager.salvaAnnotazione(annotazioneAndata)) {
            annotazioniSalvate++
        } else {
            messaggiErrore.add("Errore nel salvare l'annotazione ANDATA")
        }

        // Salva ritorno
        val annotazioneRitorno = AnnotazioneAffollamento(
            data = LocalDate.now(),
            orario = LocalTime.now(),
            livelloAffollamento = livelloSelezionatoRitorno,
            direzione = "RITORNO"
        )

        if (databaseManager.salvaAnnotazione(annotazioneRitorno)) {
            annotazioniSalvate++
        } else {
            messaggiErrore.add("Errore nel salvare l'annotazione RITORNO")
        }

        // Mostra risultato
        if (annotazioniSalvate == 2) {
            // Reset selezioni
            livelloSelezionatoAndata = 0
            livelloSelezionatoRitorno = 0
            resetStilePulsanti(pulsantiAndataAffollamento)
            resetStilePulsanti(pulsantiRitornoAffollamento)

            mostraAlert("Successo", "Annotazioni per andata e ritorno salvate correttamente!")
        } else {
            val messaggioCompleto = "Errore nel salvare le annotazioni:\n" + messaggiErrore.joinToString("\n")
            mostraAlert("Errore", messaggioCompleto)
        }
    }

    private fun resetStilePulsanti(pulsanti: List<Button>) {
        pulsanti.forEach { pulsante ->
            val originalColor = when (pulsante.text) {
                "1" -> "#4CAF50"
                "2" -> "#8BC34A"
                "3" -> "#FFC107"
                "4" -> "#FF9800"
                "5" -> "#F44336"
                else -> "#CCCCCC"
            }
            pulsante.style = "-fx-background-color: $originalColor; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"
        }
    }

    private fun mostraReport() {
        val annotazioni = databaseManager.caricaAnnotazioni()

        if (annotazioni.isEmpty()) {
            mostraAlert("Report", "Nessuna annotazione presente.")
            return
        }

        val reportWindow = ReportWindow(annotazioni)
        reportWindow.show()
    }

    private fun mostraAlert(titolo: String, messaggio: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = titolo
        alert.headerText = null
        alert.contentText = messaggio
        alert.showAndWait()
    }
}
