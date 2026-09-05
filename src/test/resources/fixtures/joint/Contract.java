package joint;

import liquidjava.specification.Refinement;

class Contract {
    static void positive(@Refinement("_ > 0") int value) {}
}
