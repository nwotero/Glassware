from bluetooth import *
import traceback

server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "1aefbf9b-ea60-47de-b5a0-ed0e3a36d9a5"
testUuid = "00001101-0000-1000-8000-00805F9B34FB"

advertise_service( server_sock, "GlassServer",
                   service_id = testUuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ], 
#                   protocols = [ OBEX_UUID ] 
                    )
                   
print("Waiting for connection on RFCOMM channel %d" % port)

client_sock, client_info = server_sock.accept()
print("Accepted connection from ", client_info)

try:
    while True:
        data = client_sock.recv(1024)
        if len(data) == 0:
            pass
            #break
        else:
            print("received: \"%s\"" % data)
            if data == "end connection\n": break
            #client_sock.send("received [%s]" % data)
except IOError:
    print "Error, disconnecting"
    traceback.print_exc()

print("disconnected")

client_sock.close()
server_sock.close()
print("all done")
