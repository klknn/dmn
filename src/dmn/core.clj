(ns dmn.core
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.java.io :as io])
  (:import (javax.swing JList JFrame JScrollPane JPanel)
           (java.util Vector)))

(defn maybe-parse-xml [f]
  "Returns nil with message if failed."
  (try
    (xml/parse f)
    (catch org.xml.sax.SAXException e
      (println (.getMessage e)
               "File: " (.getPath f)
               "Line:" (.getLineNumber e)
               "Col:" (.getColumnNumber e)))))

(defn make-soundbank [root]
  "Converts a list of (:tag :contents) to map {:tag :contents} in root.
   For example, keys are :InstrumentsList, :DrumSetList."
  (let [init (xml/attrs root)]
    (reduce (fn [m c] (assoc m (xml/tag c) (xml/content c)))
            (assoc init :FullName (format "%s/%s" (:Folder init) (:Name init)))
            (xml/content root))))

(def testbank
  (-> "Module/ok_XGb.xml" io/resource io/input-stream xml/parse make-soundbank))

(def testpatch
  (->> testbank
       :InstrumentList
       first
       :content
       (map :content)
       flatten))

(def banks
  (->> (-> (io/resource "Module") io/file file-seq)
       (filter #(.endsWith (.getName %1) ".xml"))
       (map maybe-parse-xml)
       (remove nil?)
       (map make-soundbank)
       (sort-by :FullName)))

(def device-infos
  (map (fn [info]
         {:FullName (format "%s/%s" (.getVendor info) (.getName info))
          :Name (.getName info)
          :Vendor (.getVendor info)
          :Desc (.getDescription info)})
       (javax.sound.midi.MidiSystem/getMidiDeviceInfo)))

(defn -main
  "Runs this program."
  [& args]
  (println "# MIDI devices")
  (doseq [i device-infos] (println i))
  (println "# SoundBank XML")
  (doseq [b banks] (println (b :FullName)))

  (defn jlist [names]
    (->> names Vector. JList.))

  (let [frame (JFrame. "dmn")
        main-panel (JPanel.)

        dev-scroll (JScrollPane.)
        dev-names (-> (map :FullName device-infos) jlist)
        dev-panel (JPanel.)

        bank-scroll (JScrollPane.)
        bank-names (-> (map :FullName banks) jlist)
        bank-panel (JPanel.)

        inst-scroll (JScrollPane.)
        inst-names (->> (:InstrumentList testbank) (map :attrs) (map :Name) jlist)
        inst-panel (JPanel.)

        patch-scroll (JScrollPane.)
        patch-names (->> testpatch (map xml/attrs) (map :Name) jlist)
        patch-panel (JPanel.)

        send-button (javax.swing.JButton. "Send")]
    ;; setup frame
    (.setBounds frame 10 10 320 640)
    (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
    (.setVisible frame true)

    ;; setup main
    (.setLayout main-panel (javax.swing.BoxLayout.
                            main-panel javax.swing.BoxLayout/Y_AXIS))
    (.add (.getContentPane frame) main-panel java.awt.BorderLayout/CENTER)

    ;; setup banks
    (.setView (.getViewport bank-scroll) bank-names)
    (.add bank-panel bank-scroll)
    (.add main-panel bank-panel)
    (.setPreferredSize bank-scroll (java.awt.Dimension. 300 120))

    ;; setup inst
    (.setView (.getViewport inst-scroll) inst-names)
    (.add inst-panel inst-scroll)
    (.add main-panel inst-panel)
    (.setPreferredSize inst-scroll (java.awt.Dimension. 300 120))

    ;; setup patch
    (.setView (.getViewport patch-scroll) patch-names)
    (.add patch-panel patch-scroll)
    (.add main-panel patch-panel)
    (.setPreferredSize patch-scroll (java.awt.Dimension. 300 120))

    ;; setup devices
    (.setView (.getViewport dev-scroll) dev-names)
    (.add dev-panel dev-scroll)
    (.add main-panel dev-panel)
    (.setPreferredSize dev-scroll (java.awt.Dimension. 300 80))

    ;; setup button
    ;; (.addActionCommand send-button "sendButton")
    (.addActionListener
     send-button
     (reify java.awt.event.ActionListener
       (actionPerformed [this e] (println "SEND"))))
    (.add main-panel send-button)
    ))
