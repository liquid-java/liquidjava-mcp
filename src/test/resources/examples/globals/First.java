package globals;

import liquidjava.specification.*;

@RefinementAlias("FirstAlias(int x) { x > 0 }")
@Ghost("int firstGhost")
@StateSet({"firstOpen", "firstClosed"})
class First {
    void inspect(@Refinement("_ > 0") int input) {
        int local = input;
    }
}
