package examples;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;

@ExternalRefinementsFor("java.lang.Math")
interface ExternalContracts {
    @Refinement("_ == value")
    int abs(int value);
}
