package br.com.zupacademy.murilo

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("br.com.zupacademy.murilo")
		.start()
}

