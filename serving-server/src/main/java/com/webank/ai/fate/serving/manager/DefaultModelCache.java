/*
 * Copyright 2019 The FATE Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.ai.fate.serving.manager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.webank.ai.fate.serving.core.bean.Configuration;
import com.webank.ai.fate.serving.core.bean.Context;
import com.webank.ai.fate.serving.core.bean.Dict;
import com.webank.ai.fate.serving.core.exceptions.LoadModelException;
import com.webank.ai.fate.serving.federatedml.PipelineTask;
import com.webank.ai.fate.serving.interfaces.ModelCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
public class DefaultModelCache implements ModelCache {
    private static final Logger logger = LoggerFactory.getLogger(DefaultModelCache.class);
    private LoadingCache<String, PipelineTask> modelCache;

    @Autowired
    private ModelLoader modelLoader;

    public DefaultModelCache() {
        modelCache = CacheBuilder.newBuilder()
               // .expireAfterAccess(Configuration.getPropertyInt(Dict.PROPERTY_MODEL_CACHE_ACCESS_TTL), TimeUnit.HOURS)
                .maximumSize(Configuration.getPropertyInt(Dict.PROPERTY_MODEL_CACHE_MAX_SIZE,100))
                .build(
                        new CacheLoader<String, PipelineTask>() {
                    @Override
                    public PipelineTask load(String s) throws Exception {
                        return loadModel(null,s);
                    }
                });
    }

    @Override
    public PipelineTask loadModel(Context context, String modelKey) {
        String[] modelKeyFields = ModelUtil.splitModelKey(modelKey);
        PipelineTask pipelineTask = modelLoader.loadModel(context,modelKeyFields[0], modelKeyFields[1]);
        if(pipelineTask==null){
            logger.error("load model {} error,model loader return null",modelKey);
            throw  new LoadModelException();
        }
        return  pipelineTask;
    }

    @Override
    public PipelineTask get(Context  context,String modelKey) {
        try {
            return modelCache.get(modelKey);
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    @Override
    public void put(Context  context,String modelKey, PipelineTask model) {
        modelCache.put(modelKey, model);
    }

    @Override
    public long getSize() {
        return modelCache.size();
    }

    @Override
    public Set<String> getKeys() {
        return modelCache.asMap().keySet();
    }
}
