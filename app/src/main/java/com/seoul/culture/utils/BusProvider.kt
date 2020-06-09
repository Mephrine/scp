package com.seoul.culture.utils

import com.squareup.otto.Bus

class BusProvider {
    companion object {
        val instance = Bus()
    }
}