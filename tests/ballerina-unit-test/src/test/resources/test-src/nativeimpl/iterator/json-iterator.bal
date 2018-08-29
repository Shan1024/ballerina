import ballerina/io;

function testJsonIterator() {

    json data = {
        name: "Ballerina",
        age: 2
    };

    JsonIterator iterator = data.iterate();
    while (true) {
        match iterator.next() {
            record { (string, any) value; !... } rec => {
                string key = rec.value[0];
                any value = rec.value[1];

                io:println(key, " : ", value);
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

    // Json can have only one var ref
    //foreach d, e in data {
    //    io:println(d, " : ", e);
    //}
    //
    //io:println("done");
}
