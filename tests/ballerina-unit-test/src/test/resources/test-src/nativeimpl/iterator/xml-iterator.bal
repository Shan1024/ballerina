import ballerina/io;

function testXmlIterator() {

    xml bookName = xml `<name>Book1</name>`;
    xml bookComment = xml `<!--some comment-->`;
    xml someText = xml `Hello, World!`;
    xml data = someText + bookName + bookComment;
    XmlIterator iterator = data.iterate();

    while (true) {
        match iterator.next() {
            record { xml value; !... } s => {
                io:println(s.value);
            }
            () => {
                break;
            }
        }
    }

    io:println("done");
}
