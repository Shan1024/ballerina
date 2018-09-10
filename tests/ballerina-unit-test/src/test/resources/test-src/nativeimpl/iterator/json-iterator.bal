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
                var t = rec.value;
                io:println(t[0], " : ", t[1]);
            }
            () => {
                break;
            }
        }
    }

    io:println("done");

    foreach value in data {
        io:println(value[0], " : ", value[1]);
    }

    io:println("done");

    // Json can have only one var ref
    //foreach d, e in data {
    //    io:println(d, " : ", e);
    //}
    //
    //io:println("done");

}
