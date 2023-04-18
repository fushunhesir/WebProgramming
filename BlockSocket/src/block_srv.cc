#include <cstdio>
#include <cstdlib>
#include <unistd.h>
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

#define QUEUE_SIZE              10
#define PORT                    "12345"
#define BUF_SIZE                1024

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
        if((sd = socket(selected->ai_family, selected->ai_socktype, selected->ai_protocol)) == -1){
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
    freeaddrinfo(srvinfo);
    return sd;
}

int main(void){
    int sd;
    int status;
    char ip[INET_ADDRSTRLEN];


    socklen_t sin_size;
    struct sigaction sa;
    sockaddr_storage visiter;
    addrinfo hints, *srvinfo, *selected;


    sethints(AF_INET, AI_PASSIVE, SOCK_STREAM, hints);
    if((status = getaddrinfo(nullptr, PORT, &hints, &srvinfo)) != 0){
        fprintf(stderr, "getaddrinfo: fail\n");
        return 1;
    } 
    sd = create_bind_socket(srvinfo, selected);
    if(listen(sd, QUEUE_SIZE) == -1){
        fprintf(stderr, "listen: fail to listen\n");
        return 1;
    }


    sa.sa_handler = sigchld_handler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART;
    if (sigaction(SIGCHLD, &sa, NULL) == -1) {
        perror("sigaction");
        exit(1);
    }


    printf("server: waiting for connections...\n");

    while(1){
        sin_size = sizeof visiter;
        int new_fd = accept(sd, (struct sockaddr *)&visiter, &sin_size);
        if (new_fd == -1) {
          perror("accept");
          continue;
        }
        inet_ntop(visiter.ss_family, get_in_addr((struct sockaddr *)&visiter), ip, sizeof ip);
        std::cout << "server: got connection from " << ip << std::endl;
        if (!fork()) {
            close(sd);

            char buffer[BUF_SIZE];
            char* filename = "/Users/macbook/Projects/WebProgramming/BlockSocket/movie/src.mkv";
            std::ifstream file(filename, std::ios::binary);

            while(file.read(buffer, BUF_SIZE)) {
                if(send(new_fd, buffer, file.gcount(), 0) == -1) {
                    std::cerr << "Failed to send data" << std::endl;
                    return 1;
                }
            }
            if(file.gcount() > 0) {
                if (send(new_fd, buffer, file.gcount(), 0) == -1) {
                    std::cerr << "Failed to send data" << std::endl;
                    return 1;
                }
            }

            std::cout << "movie has been sent completely!" << std::endl;
            // Close file and socket
            // Close file
            file.close();
            close(new_fd);
            exit(0);
        }
    }
    return 0;
}