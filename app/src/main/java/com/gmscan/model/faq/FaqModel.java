package com.gmscan.model.faq;

/**
 * Model class representing a FAQ item.
 * Each FAQ item consists of a question, an answer, and a category.
 */
public record FaqModel(String question, String answer, String category) {
    /**
     * Constructor to initialize the FAQ item.
     *
     * @param question The FAQ question.
     * @param answer   The answer to the FAQ question.
     * @param category The category of the FAQ (e.g., General, Scan, Account).
     */
    public FaqModel {
    }

    /**
     * Gets the FAQ question.
     *
     * @return The question string.
     */
    @Override
    public String question() {
        return question;
    }

    /**
     * Gets the FAQ answer.
     *
     * @return The answer string.
     */
    @Override
    public String answer() {
        return answer;
    }

    /**
     * Gets the category of the FAQ.
     *
     * @return The category string.
     */
    @Override
    public String category() {
        return category;
    }
}
