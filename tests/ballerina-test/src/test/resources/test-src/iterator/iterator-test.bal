import ballerina/io;

function test() {

    string[] data = ["A", "B", "C"];

    ArrayIterator|error arrayIterator = <ArrayIterator>iterator(data);

    match arrayIterator {
        ArrayIterator ai => {
            {(int, any) value;}? nextEntry = ai.Next();
            while (true){
                match nextEntry {
                    () => {
                        break;
                    }
                    {(int, any) value;} entry => {
                        io:println(entry);
                        nextEntry = ai.Next();
                    }
                }
            }
        }
        error e => {
            io:println(e);
        }
    }
}
