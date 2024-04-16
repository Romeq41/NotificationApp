class InvalidTimeFormatException extends Exception {

    @Override
    public String getMessage() {
        return "Niepoprawny Format Czasu. Wymagany format : HH:MM:SS";
    }

}
