import ballerina/io;

function testStringIterator() {

    string name = "Ballerina";
    StringIterator iterator = name.iterate();

    while (true) {
        match iterator.next() {
            record { string value; !... } s => {
                io:println(s.value);
            }
            () => {
                break;
            }
        }
    }

    io:println("done");
}
