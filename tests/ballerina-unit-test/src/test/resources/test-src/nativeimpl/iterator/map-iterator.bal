import ballerina/io;

function testMapIterator() {

    //string key;
    //any value;
    //
    //var iterator = getData().iterate();
    //while (true) {
    //    match iterator.next() {
    //        record { string key; any value; !... } rec => {
    //
    //            key = rec.key;
    //            value = rec.value;
    //
    //            io:print(key + " : ");
    //            io:println(key);
    //        }
    //        () => {
    //            break;
    //        }
    //    }
    //}

    foreach key, value in getData() {
        io:println(key, " : ", value);
    }
}

function getData() returns map {
    map data = { "name": "Ballerina", "age": 10 };
    return data;
}
