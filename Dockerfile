FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    curl \
    ruby \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

RUN apt-get update

# --------------------------------

ARG ARG_USER
ARG ARG_GROUP
ARG ARG_HOME=/home/${ARG_USER}

RUN groupadd ${ARG_USER} \
  && useradd ${ARG_USER} -g ${ARG_GROUP} -m

USER ${ARG_USER}

# --------------------------------

RUN mkdir ${ARG_HOME}/bin
ENV PATH=${ARG_HOME}/bin:${PATH}

WORKDIR ${ARG_HOME}/bin

RUN curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz \
  | gzip -d > cs

RUN chmod +x cs

RUN ./cs setup --yes

RUN echo '. ~/.profile' > ${ARG_HOME}/.bash_profile
RUN echo "export COURSIER_CACHE=${ARG_HOME}/work/.coursier_cache" >> ${ARG_HOME}/.profile

SHELL ["/bin/bash", "-l", "-c"]

# 初回実行時にここで何かインストールされるっぽい
RUN sbt --version

# --------------------------------

WORKDIR ${ARG_HOME}/work

ENV IN_CONTAINER=1
ENV LANG=en_US.UTF-8

CMD ["bash", "-l"]
