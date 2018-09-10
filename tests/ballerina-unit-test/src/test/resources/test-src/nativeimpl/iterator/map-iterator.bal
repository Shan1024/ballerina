import ballerina/io;

function testMapIterator() {

    var iterator = getData().iterate();
    while (true) {
        match iterator.next() {
            record { (string, any) value; !... } rec => {

                var t = rec.value;

                io:println(t[0], " : ", t[1]);
            }
            () => {
                break;
            }
        }
    }

    io:println("done");

    foreach value in getData() {
        io:println(value[0], " : ", value[1]);
    }

    io:println("done");

}

function getData() returns map {
    map data = { "name": "Ballerina", "age": 10 };
    return data;
}
