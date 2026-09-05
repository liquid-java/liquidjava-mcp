import liquidjava.specification.*;

@RefinementAlias("Positive(int x) { x > 0 }")
@Ghost("int size")
@StateSet({"open", "closed"})
class Context {
    void run(@Refinement("_ > 0") int input) {
        int first = input;
        if (input > 1) {
            int nested = 2;
        }
        int last = 3;
    }
    void other() {
        int unrelated = 4;
    }
}
