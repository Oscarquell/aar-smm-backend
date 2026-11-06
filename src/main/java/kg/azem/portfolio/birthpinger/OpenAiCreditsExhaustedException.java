package kg.azem.portfolio.birthpinger;

public class OpenAiCreditsExhaustedException extends RuntimeException {
    public OpenAiCreditsExhaustedException() {
        super("OpenAI API credits are exhausted");
    }
}
