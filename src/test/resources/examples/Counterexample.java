import liquidjava.specification.Refinement;

class Counterexample {
    @Refinement("_ > 0")
    int positive(int value) {
        return value;
    }
}
