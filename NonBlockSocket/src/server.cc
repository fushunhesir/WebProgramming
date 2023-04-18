#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#include <cstring>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <sys/wait.h>
#include <signal.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <thread>

#define QUEUE_SIZE              10
#define PORT                    "12345"
#define BUF_SIZE                1024
#define FILE_PATH               "./moive/src.mkv"

void sigchld_handler(int s)
{
  while(waitpid(-1, NULL, WNOHANG) > 0);
}

void *get_in_addr(struct sockaddr *sa)
{
  if (sa->sa_family == AF_INET) {
    return &(((struct sockaddr_in*)sa)->sin_addr);
  }
  return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

void sethints(int family, int flag, int socktype, addrinfo& hints){
    memset(&hints, 0, sizeof hints);
    hints.ai_family = family;
    hints.ai_flags = flag;
    hints.ai_socktype = socktype;
}

int create_bind_socket(addrinfo* srvinfo, addrinfo* selected){
    int sd = 0;
    int yes = 1;

    for(selected = srvinfo; selected != nullptr; selected = selected->ai_next){
        if((sd = socket(selected->ai_family, selected->ai_socktype, 
        selected->ai_protocol)) == -1){
            fprintf(stderr, "socket: fail to allocate socket\n");
            continue;
        }
        if (setsockopt(sd, SOL_SOCKET, SO_REUSEADDR, &yes,sizeof(int)) == -1) {
             fprintf(stderr, "setsockopt: %d\n", errno);
             exit(1);
        }
        if(bind(sd, selected->ai_addr, selected->ai_addrlen) == -1){
            close(sd);
            fprintf(stderr, "bind: error number is:%d\n", errno);
            continue;
        }
        break;
    }

    fcntl(sd, F_SETFL, O_NONBLOCK);
    freeaddrinfo(srvinfo);
    return sd;
}

int get_listener() {
    int sd;
    addrinfo hints, *srvinfo, *selected;

    sethints(AF_INET, AI_PASSIVE, SOCK_STREAM, hints);
    if(getaddrinfo(nullptr, PORT, &hints, &srvinfo) != 0){
        fprintf(stderr, "getaddrinfo: fail\n");
        return 1;
    } 
    sd = create_bind_socket(srvinfo, selected);
    return sd;
}

int client_handler(int sd) {
    fd_set wtset;
    char buffer[BUF_SIZE];
    int remains = 0;
    
    FD_ZERO(&wtset);
    FD_SET(sd, &wtset);

    while(1) {
        if(select(sd + 1, nullptr, &wtset, nullptr, nullptr) == -1){
            perror("thread select");
            exit(0);
        }
        std::ifstream file(FILE_PATH, std::ios::binary);
        while(file.read(buffer + remains, BUF_SIZE - remains));{
            int num = send(sd, buffer, BUF_SIZE, 0);
            if(num < BUF_SIZE) {
                memmove(buffer, buffer + num, BUF_SIZE - num);
                remains = BUF_SIZE - num;
                break;
            }
        }
        if(file.eof()) break;
    }
    return 0;
}

int main(void){
    int listener;
    int fdmax;
    int new_fd;
    char ip[INET_ADDRSTRLEN];
    fd_set r_master, w_master,rdset, wtset;

    socklen_t sin_size;
    struct sigaction sa;
    sockaddr_storage visiter;

    listener = get_listener();
    

    if(listen(listener, QUEUE_SIZE) == -1){
        fprintf(stderr, "listen: fail to listen\n");
        return 1;
    }

    // handle sigchild
    sa.sa_handler = sigchld_handler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART;
    if (sigaction(SIGCHLD, &sa, NULL) == -1) {
        perror("sigaction");
        exit(1);
    }


    printf("server: waiting for connections...\n");

    FD_ZERO(&r_master);
    FD_ZERO(&rdset);
    FD_ZERO(&w_master);
    FD_ZERO(&wtset);
    FD_SET(listener, &r_master);
    fdmax = listener + 1;

    while(1){
        rdset = r_master;
        wtset = w_master;
        if(select(fdmax, &rdset, &wtset, nullptr, nullptr) == -1) {
            perror("select");
            exit(2);
        }

        int max = fdmax + 1;
        for(int i = 0; i < max; i++){
            if(FD_ISSET(i, &rdset)){
                // new connection
                sin_size = sizeof visiter;
                new_fd = accept(listener, (struct sockaddr *)&visiter, &sin_size);
                if (new_fd == -1) {
                  perror("accept");
                  continue;
                }
                fdmax = new_fd + 1;
                FD_SET(new_fd, &w_master);
                fcntl(new_fd, F_SETFL, O_NONBLOCK);
                inet_ntop(visiter.ss_family, get_in_addr((struct sockaddr *)&visiter), ip, sizeof ip);
                std::cout << "server: got connection from " << ip << std::endl;
            } else if(FD_ISSET(i, &wtset)){
                std::thread td(client_handler, i);
                FD_CLR(i, &w_master);
                td.detach();
            }
        }
    }
    return 0;
}
