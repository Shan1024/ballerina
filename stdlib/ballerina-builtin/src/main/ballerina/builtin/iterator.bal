public native function iterator(any a) returns any;

public type ArrayIterator object {

    private {
        any data;
    }

    public new(data) {

    }

    public native function next() returns {(int, any) value;}?;
};
