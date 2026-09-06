package globals;

import liquidjava.specification.*;

@RefinementAlias("SecondAlias(int x) { x < 0 }")
@Ghost("int secondGhost")
@StateSet({"secondOpen", "secondClosed"})
class Second {}
