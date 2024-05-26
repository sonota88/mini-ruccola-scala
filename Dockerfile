FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    curl \
    ruby \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

# --------------------------------

ARG user
ARG group
ARG ARG_HOME=/home/${user}

RUN groupadd ${user} \
  && useradd ${user} -g ${group} -m

USER ${user}

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
