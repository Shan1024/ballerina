import ballerina/io;

function testMapIterator() {

    map data = {"name": "Ballerina", "age": 10};
    MapIterator iterator = data.iterate();

    while (true) {
        match iterator.next() {
            record { string key; any value; !... } s => {
                io:print(s.key + " : ");
                io:println(s.value);
            }
            () => {
                break;
            }
        }
    }

    io:println("done");
}
