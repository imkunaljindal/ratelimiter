public boolean rateLimit(Integer customerId) {
        long currentTimeInMillis = System.currentTimeMillis();
        ClientConfig clientConfig = clientStore.computeIfAbsent(customerId, k -> new ClientConfig(0,currentTimeInMillis, 0));
        long startTimeBucket = (currentTimeInMillis/windowSizeInMillis)*windowSizeInMillis;

        long timeDiff = getTimeDiff(currentTimeInMillis, startTimeBucket);
        if(clientConfig.getLatestTimestamp() <= startTimeBucket) clientConfig.setRequestsMade(0);

        if(clientConfig.getRequestsMade() < this.maxAllowedRequests){
            clientConfig = getClientConfig(clientConfig.getRequestsMade()+1,
                                           currentTimeInMillis, clientConfig.getCreditBalance());
            clientStore.put(customerId,clientConfig);
            return true;
        }
        else if(clientConfig.getCreditBalance() > 0){
            clientConfig = getClientConfig(clientConfig.getRequestsMade()+1,currentTimeInMillis,
                                           clientConfig.getCreditBalance()-1);
            clientStore.put(customerId,clientConfig);
        }
        else{
            int carryOverCredits = this.maxAllowedRequests > clientConfig.getRequestsMade() ? (this.maxAllowedRequests- clientConfig.getRequestsMade()): 0;
            clientConfig = getClientConfig(1,currentTimeInMillis,Math.min(creditCap,
                                                                          clientConfig.getCreditBalance())+carryOverCredits);
            clientStore.put(customerId, clientConfig);
            return true;
        }
        return false;
    }
