import ballerina/io;

type Employee record {
    int id,
    string name,
    float salary,
};

function testTableIterator() {

    table<Employee> data = table {
        { primarykey id, name, salary },
        [
            {1, "Mary", 300.5},
            {2, "John", 200.5},
            {3, "Jim", 330.5}
        ]
    };

    TableIterator iterator = data.iterate();

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
}
