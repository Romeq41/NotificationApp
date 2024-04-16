public class ThisTimeHasAlreadyPassedException extends Exception {

    @Override
    public String getMessage() {
        return "Ten czas już minął. Podaj godzinę która jeszcze nie wystąpiła";
    }
}
