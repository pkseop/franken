@if "%1" == "" goto usage

mvn archetype:generate -DarchetypeGroupId=kr.co.future -DarchetypeArtifactId=franken-template -DarchetypeVersion=1.0.1 -DgroupId=kr.co.future -DartifactId=%1 -Dversion=1.0.0 -Dpackage=%2

@goto end

:usage

@echo usage: %0 artifactId package

:end
