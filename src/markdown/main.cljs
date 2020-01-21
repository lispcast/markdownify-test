(ns markdown.main
  (:require [reagent.core :as reagent]
            ["showdown" :as showdown]))

(defonce flash-message (reagent/atom nil))
(defonce flash-timeout (reagent/atom nil))

(defn flash [text ms]
  (js/clearTimeout @flash-timeout)
  (reset! flash-message text)
  (reset! flash-timeout (js/setTimeout #(reset! flash-message nil) ms)))

(defonce text-state (reagent/atom {:format :md
                                   :value ""}))

(defonce converter (showdown/Converter.))

(defn html->md [html]
  (.makeMd converter html))

(defn md->html [md]
  (.makeHtml converter md))

(defn ->html [{:keys [format value]}]
  (case format
    :md (md->html value)
    :html value
    (str "Unknown format: " format)))

(defn ->md [{:keys [format value]}]
  (case format
    :md value
    :html (html->md value)
    (str "Unknown format: " format)))

(defn copy-to-clipboard [str]
  (let [el (.createElement js/document "textarea")
        selection (when (pos? (-> js/document .getSelection .-rangeCount))
                    (-> js/document .getSelection (.getRangeAt 0)))]
    (set! (.-value el) str)
    (.setAttribute el "readonly", "")
    (set! (-> el .-style .-position) "absolute")
    (set! (-> el .-style .-left) "-9999px")
    (-> js/document .-body (.appendChild el))
    (.select el)
    (.execCommand js/document "copy")
    (-> js/document .-body (.removeChild el))
    (when selection
      (-> js/document .getSelection .removeAllRanges)
      (-> js/document .getSelection (.addRange selection))))
  nil)

(defn md-editor []
  [:div
   {:style {:height "100%"
            :display :flex
            :flex-direction :column}}
   [:h2 "Markdown"]
   [:div
    {:style {:padding-bottom "1em"
             :flex "1"}}
    [:textarea
     {:style {:resize "none"
              :height "100%"
              :width "100%"}
      :value (->md @text-state)
      :on-change #(reset! text-state {:format :md
                                      :value  (-> % .-target .-value)})}]]
   [:button
    {:style {:border-radius 10
             :background-color :green
             :color :white
             :padding "1em"
             :border 0
             :margin-right :auto}
     :on-click (fn []
                 (copy-to-clipboard (->md @text-state))
                 (flash "Markdown copied to clipboard." 3000))}
    "Copy Markdown"]])

(defonce ui-state (reagent/atom {}))

(defn html-editor []
  [:div
   {:style {:height "100%"
            :display :flex
            :flex-direction :column}}
   [:h2 "HTML"]
   [:div
    {:style {:flex "1"
             :padding-bottom "1em"}}
    [:textarea
     {:style {:resize "none"
              :height "100%"
              :width "100%"}
      :value (->html @text-state)
      :on-change #(reset! text-state {:format :html
                                      :value  (-> % .-target .-value)})}]]
   [:button
    {:style {:border-radius 10
             :background-color :green
             :color :white
             :padding "1em"
             :border 0
             :margin-right :auto}
     :on-click (fn []
                 (copy-to-clipboard (->html @text-state))
                 (flash "HTML copied to clipboard." 3000))}
    "Copy HTML"]])

(defn preview []
  [:div
   {:style {:overflow-y :scroll
            :height "100%"
            :max-width "100%"}
    :dangerouslySetInnerHTML {:__html (->html @text-state)}}])

(defn app []
  [:div {:style {:position :relative}}
   [:div {:style {:position :absolute
                  :z-index 100
                  :text-align :center
                  :width "50%"
                  :min-width "200px"
                  :margin :auto
                  :left 0
                  :right 0
                  :background-color :yellow
                  :padding "2em"
                  :overflow :hidden
                  :box-sizing :border-box
                  :transform (if @flash-message
                               "scaleY(1)"
                               "scaleY(0)")
                  :transition "transform 0.2s ease-out"}}
    @flash-message]
   [:div
    {:style {:padding "2em"}}

    [:h1 "Markdownify"]
    [:div
     {:style {:display :flex}}
     [:div {:style {:flex "1"
                    :padding-right "2em"
                    :height 500}}
      [md-editor]]
     [:div {:style {:flex "1"
                    :padding-right "2em"
                    :height 500}}

      [html-editor]]
     [:div {:style {:flex "1"
                    :padding-left "2em"}}
      [:h2 "Rendered HTML"]
      [:div
       {:style {:height 500
                :padding-bottom "1em"
                :overflow :hidden}}
       [preview]]]]
    [:div (pr-str @ui-state)]]])

(defn mount []
  (reagent/render [app]
                  (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload! []
  (mount))
