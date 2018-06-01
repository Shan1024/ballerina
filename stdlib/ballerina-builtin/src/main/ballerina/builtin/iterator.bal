public native function iterator(any a) returns any;

public type ArrayIterator object {
    // Todo - Rename to next later(next is still a keyword)
    public native function Next() returns {(int, any) value;}?;
    //public native function Next() returns any;
};
