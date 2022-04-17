package com.xobotun.tinkoff.cabel

import com.github.badoualy.telegram.api.TelegramApiStorage
import com.github.badoualy.telegram.mtproto.auth.AuthKey
import com.github.badoualy.telegram.mtproto.model.DataCenter
import com.github.badoualy.telegram.mtproto.model.MTSession

class InMemoryApiStorage : TelegramApiStorage {
    private var dataCenter: DataCenter? = null
    private var session: MTSession? = null
    private var authKey: AuthKey? = null

    override fun loadAuthKey(): AuthKey?  = authKey
    override fun saveAuthKey(authKey: AuthKey) {
        this.authKey = authKey
    }
    override fun deleteAuthKey() {
        authKey = null
    }

    override fun loadDc(): DataCenter? = dataCenter
    override fun saveDc(dataCenter: DataCenter) {
        this.dataCenter = dataCenter
    }
    override fun deleteDc() {
        dataCenter = null
    }

    override fun loadSession(): MTSession? = session
    override fun saveSession(session: MTSession?) {
        this.session = session
    }



}
