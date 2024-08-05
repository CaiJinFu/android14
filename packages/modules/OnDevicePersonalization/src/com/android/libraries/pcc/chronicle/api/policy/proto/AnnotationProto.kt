// /*
//  * Copyright (C) 2022 The Android Open Source Project
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package com.android.libraries.pcc.chronicle.api.policy.proto

// import arcs.core.data.proto.AnnotationParamProto
// import arcs.core.data.proto.AnnotationProto
// import com.android.libraries.pcc.chronicle.api.policy.annotation.Annotation
// import com.android.libraries.pcc.chronicle.api.policy.annotation.AnnotationParam

// private fun AnnotationParamProto.decode(): AnnotationParam {
//   return when (valueCase) {
//     AnnotationParamProto.ValueCase.STR_VALUE -> AnnotationParam.Str(strValue)
//     AnnotationParamProto.ValueCase.NUM_VALUE -> AnnotationParam.Num(numValue)
//     AnnotationParamProto.ValueCase.BOOL_VALUE -> AnnotationParam.Bool(boolValue)
//     else -> throw UnsupportedOperationException("Invalid [AnnotationParam] type $valueCase.")
//   }
// }

// private fun AnnotationParam.encode(paramName: String): AnnotationParamProto {
//   val proto = AnnotationParamProto.newBuilder().setName(paramName)
//   when (this) {
//     is AnnotationParam.Bool -> proto.boolValue = value
//     is AnnotationParam.Str -> proto.strValue = value
//     is AnnotationParam.Num -> proto.numValue = value
//   }
//   return proto.build()
// }

// /** Converts a [AnnotationProto] into a [Annotation]. */
// fun AnnotationProto.decode(): Annotation {
//   return Annotation(name = name, params = paramsList.associate { it.name to it.decode() })
// }

// /** Converts a [Annotation] into a [AnnotationProto]. */
// fun Annotation.encode(): AnnotationProto {
//   return AnnotationProto.newBuilder()
//     .setName(name)
//     .addAllParams(params.map { (name, param) -> param.encode(name) })
//     .build()
// }
