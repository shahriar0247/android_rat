import socket
import math
import time

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

def recv2(s):
    buffer = s.recv(32).decode("utf-8").replace("buffer: ", "")
    normal_buffer = 1024
    buffer = int(buffer) + 1
    send(s, "ok")
    times_to_recieve = buffer / normal_buffer
    print(times_to_recieve)
    output = ""
    times_to_recieve = math.ceil(times_to_recieve)
    print("number of times to recieve "+ str(times_to_recieve))
    for a in range(0, times_to_recieve):
        print("reciving: " + str(a))
        new_output = s.recv(normal_buffer).decode("utf-8")
        output = output + new_output
        print(len(output))
  

    return output

def recv(s):
    buffer = s.recv(32).decode("utf-8").replace("buffer: ", "")
    new_buffer = 1025
    buffer = int(buffer) + 1
    send(s, "ok")
    times_to_recieve = buffer / new_buffer

    output = ""
   
    while len(output) != buffer:
        
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

def get_input(client):
    cmd = input("cmd -> ")

    send(client,cmd)
    output = (recv(client))
    return cmd, output

def commands(cmd, output, client):
    if output.startswith("currentpath:") and cmd.startswith("cd "):

        newpath = cd(cmd,output, client) 
        send(client, "newpath:" + newpath)

        output = recv(client)
    if output.startswith("download "):
        filesize = int(client.recv(1024).decode("utf-8"))
        print("filesize: " +str(filesize))
        send("ok")
        data = client.recv(1024)
        while len(data) != filesize:
            data = data + client.recv(1024)

        with open("tasker.apk") as a:
            a.write(data)
    return output

try:

    sock = create_socket("10.9.11.18",4422)
    client, address = connect(sock)

    print(recv(client))
    send(client,"hey sup")

    while True:
    
        cmd, output = get_input(client)
        output = commands(cmd, output, client)
        print(output)


except KeyboardInterrupt:     
    exit(1)
