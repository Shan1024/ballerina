public native function iterator(any a) returns any;

public type ArrayIterator object {

    public native function next() returns {(int, any) value;}?;

};
