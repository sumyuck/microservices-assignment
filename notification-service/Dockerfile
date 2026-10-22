FROM mcr.microsoft.com/dotnet/sdk:8.0-alpine AS build
WORKDIR /src
COPY . .
RUN dotnet publish -c Release -o /out
FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine
WORKDIR /app
COPY --from=build /out .
RUN adduser -D app && chown -R app:app /app
USER app
EXPOSE 8085
ENTRYPOINT ["dotnet", "NotificationService.dll"]
