import liquidjava.specification.Refinement;

class Valid {
    void run() {
        @Refinement("_ > 0") int value = 1;
    }
}
