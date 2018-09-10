import ballerina/io;

function testStringIterator() {

    string name = "Ballerina";

    StringIterator iterator = name.iterate();

    while (true) {
        match iterator.next() {
            record { string value; !... } rec => {
                var t = rec.value;
                io:println(t);
            }
            () => {
                break;
            }
        }
    }

    // todo - add 2 var refs? index and element?
    foreach c in name {
        io:println(c);
    }
}
