import ballerina/io;

function arrayIteratorTest() {

    string[] data = ["A", "B", "C"];

    var arrayIterator = <ArrayIterator>iterator(data);

    match arrayIterator {
        ArrayIterator ai => {
            {(int, any) value;}? nextEntry = ai.next();
            while (true){
                match nextEntry {
                    () => {
                        break;
                    }
                    {(int, any) value;} entry => {
                        io:println(entry);
                        nextEntry = ai.next();
                    }
                }
            }
        }
        error e => {
            io:println(e);
        }
    }
}

type Counter object {

    private {
        any data;
    }

    public new(data) {

    }

    function __iterator() returns any {
        ArrayIterator arrayIterator = new(data);
        return arrayIterator;
    }
};

function objectIteratorTest() {

    int[] data = [10, 20, 30];

    Counter c = new(data);

    var myIterator = <ArrayIterator>iterator(c);

    match myIterator {
        ArrayIterator ai => {
            {(int, any) value;}? nextEntry = ai.next();
            while (true){
                match nextEntry {
                    () => {
                        break;
                    }
                    {(int, any) value;} entry => {
                        io:println(entry);
                        nextEntry = ai.next();
                    }
                }
            }
        }
        error e => {
            io:println(e);
        }
    }
}
