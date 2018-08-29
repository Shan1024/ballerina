import ballerina/io;

function testMapIterator() {

    //string key;
    //any value;
    //
    //var iterator = getData().iterate();
    //while (true) {
    //    match iterator.next() {
    //        record { (string, any) value; !... } rec => {
    //
    //            key = rec.value[0];
    //            value = rec.value[1];
    //
    //            io:print(key + " : ");
    //            io:println(value);
    //        }
    //        () => {
    //            break;
    //        }
    //    }
    //}
    //
    //io:println("done");

    foreach key, value in getData() {
        io:println(key, " : ", value);
    }

    io:println("done");

}

function getData() returns map {
    map data = { "name": "Ballerina", "age": 10 };
    return data;
}
