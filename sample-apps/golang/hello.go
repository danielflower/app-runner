package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/gorilla/mux"
)

func main() {

	port := os.Getenv("APP_PORT")
	appName := os.Getenv("APP_NAME")
	router := mux.NewRouter().StrictSlash(true)
	router.HandleFunc("/" + appName, Index)
	router.HandleFunc("/" + appName + "/todos", TodoIndex)
	router.HandleFunc("/" + appName + "/todos/{todoId}", TodoShow)

	log.Fatal(http.ListenAndServe(":"+port, router))
}

func Index(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintln(w, "Welcome!")
}

func TodoIndex(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintln(w, "Todo Index!")
}

func TodoShow(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	todoId := vars["todoId"]
	fmt.Fprintln(w, "Todo show:", todoId)
}