import ballerina/io;

function testJsonIterator() {

    json data = {
        name: "Ballerina",
        age: 2
    };

    JsonIterator iterator = data.iterate();
    while (true) {
        match iterator.next() {
            record { any value; !... } s => {
                io:println(s.value);
            }
            () => {
                break;
            }
        }
    }

    io:println("done");

    foreach d in data {
        io:println(d);
    }

    io:println("done");
}
