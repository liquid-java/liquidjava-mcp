import liquidjava.specification.Refinement;

class Invalid {
    void run() {
        @Refinement("_ > 0") int value = -1;
    }
}
