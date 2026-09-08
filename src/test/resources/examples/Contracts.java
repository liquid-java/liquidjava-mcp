package examples;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
class Contracts {
    @StateRefinement(to = "open(this)")
    Contracts() {}

    @Refinement("_ == value + 1")
    int increment(@Refinement("_ > 0") int value) {
        return value + 1;
    }

    void increment(@Refinement("_ >= 0") long value) {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    void close() {}
}
