/** Interface defining safety eligibility checks for a ride. */
public interface Restrictable {

    /** @return true if the visitor satisfies every safety restriction. */
    boolean isEligible(Visitor visitor);

    /** Throws a RestrictionViolationException describing the first violated rule. */
    void checkEligibility(Visitor visitor) throws RestrictionViolationException;
}
