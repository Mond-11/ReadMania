package ua.readmania.common;

/**
 * Record representing an individual field validation error in Problem Details response.
 *
 * @param field   name of the invalid field/parameter
 * @param message validation failure message (including reason, example and recommendation)
 */
public record ValidationError(String field, String message) {
}
