package org.example.seatour

// ReportWindow.kt
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.geometry.Insets
import java.time.format.DateTimeFormatter

class ReportWindow(private val annotazioni: List<AnnotazioneAffollamento>) {

    fun show() {
        val stage = Stage()
        stage.title = "Report Affollamento Bus"

        val root = VBox(10.0)
        root.padding = Insets(20.0)

        // Titolo
        val titolo = Label("Report Annotazioni Affollamento")
        titolo.style = "-fx-font-size: 20px; -fx-font-weight: bold;"

        // Statistiche
        val stats = creaStatistiche()

        // Lista annotazioni
        val listaAnnotazioni = creaListaAnnotazioni()

        root.children.addAll(titolo, stats, Separator(), listaAnnotazioni)

        val scene = Scene(root, 600.0, 500.0)
        stage.scene = scene
        stage.show()
    }

    private fun creaStatistiche(): VBox {
        val statsBox = VBox(5.0)

        val totaleAnnotazioni = annotazioni.size
        val annotazioniAndata = annotazioni.filter { it.direzione == "ANDATA" }
        val annotazioniRitorno = annotazioni.filter { it.direzione == "RITORNO" }

        val mediaAffollamentoTotale = if (annotazioni.isNotEmpty()) {
            annotazioni.map { it.livelloAffollamento }.average()
        } else 0.0

        val mediaAndataAffollamento = if (annotazioniAndata.isNotEmpty()) {
            annotazioniAndata.map { it.livelloAffollamento }.average()
        } else 0.0

        val mediaRitornoAffollamento = if (annotazioniRitorno.isNotEmpty()) {
            annotazioniRitorno.map { it.livelloAffollamento }.average()
        } else 0.0

        val labelTotale = Label("Totale annotazioni: $totaleAnnotazioni")
        val labelAndata = Label("Annotazioni andata: ${annotazioniAndata.size}")
        val labelRitorno = Label("Annotazioni ritorno: ${annotazioniRitorno.size}")
        val labelMediaTotale = Label("Media affollamento totale: %.1f".format(mediaAffollamentoTotale))
        val labelMediaAndata = Label("Media affollamento andata: %.1f".format(mediaAndataAffollamento))
        val labelMediaRitorno = Label("Media affollamento ritorno: %.1f".format(mediaRitornoAffollamento))

        statsBox.children.addAll(
            labelTotale,
            labelAndata,
            labelRitorno,
            labelMediaTotale,
            labelMediaAndata,
            labelMediaRitorno
        )
        return statsBox
    }

    private fun creaListaAnnotazioni(): ScrollPane {
        val vbox = VBox(5.0)

        val annotazioniOrdinate = annotazioni.sortedByDescending { it.data }

        for (annotazione in annotazioniOrdinate) {
            val dataFormattata = annotazione.data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val orarioFormattato = annotazione.orario.format(DateTimeFormatter.ofPattern("HH:mm"))

            val livelloColore = when (annotazione.livelloAffollamento) {
                1 -> "#4CAF50"
                2 -> "#8BC34A"
                3 -> "#FFC107"
                4 -> "#FF9800"
                5 -> "#F44336"
                else -> "#CCCCCC"
            }

            val direzioneColore = if (annotazione.direzione == "ANDATA") "#2196F3" else "#FF5722"

            val hbox = HBox(10.0)
            hbox.style = "-fx-padding: 5px; -fx-border-color: #CCCCCC; -fx-border-width: 1px;"

            val labelData = Label(dataFormattata)
            labelData.prefWidth = 100.0

            val labelOrario = Label(orarioFormattato)
            labelOrario.prefWidth = 80.0

            val labelDirezione = Label(annotazione.direzione)
            labelDirezione.style = "-fx-background-color: $direzioneColore; -fx-text-fill: white; -fx-padding: 2px 6px; -fx-background-radius: 3px; -fx-font-size: 12px;"
            labelDirezione.prefWidth = 80.0

            val labelLivello = Label("Livello ${annotazione.livelloAffollamento}")
            labelLivello.style = "-fx-background-color: $livelloColore; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-background-radius: 3px;"

            hbox.children.addAll(labelData, labelOrario, labelDirezione, labelLivello)
            vbox.children.add(hbox)
        }

        val scrollPane = ScrollPane(vbox)
        scrollPane.prefHeight = 300.0
        scrollPane.isFitToWidth = true

        return scrollPane
    }
}
