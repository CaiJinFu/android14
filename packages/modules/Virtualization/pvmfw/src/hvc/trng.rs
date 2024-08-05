// Copyright 2023, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use core::fmt;
use core::result;

/// Standard SMCCC TRNG error values as described in DEN 0098 1.0 REL0.
#[derive(Debug, Clone)]
pub enum Error {
    /// The call is not supported by the implementation.
    NotSupported,
    /// One of the call parameters has a non-supported value.
    InvalidParameter,
    /// Call returned without the requested entropy bits.
    NoEntropy,
    /// Negative values indicate error.
    Unknown(i64),
    /// The call returned a positive value when 0 was expected.
    Unexpected(u64),
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            Self::NotSupported => write!(f, "SMCCC TRNG call not supported"),
            Self::InvalidParameter => write!(f, "SMCCC TRNG call received non-supported value"),
            Self::NoEntropy => write!(f, "SMCCC TRNG call returned no entropy"),
            Self::Unexpected(v) => write!(f, "Unexpected SMCCC TRNG return value {} ({0:#x})", v),
            Self::Unknown(e) => write!(f, "Unknown SMCCC TRNG return value {} ({0:#x})", e),
        }
    }
}

impl From<i64> for Error {
    fn from(value: i64) -> Self {
        match value {
            -1 => Error::NotSupported,
            -2 => Error::InvalidParameter,
            -3 => Error::NoEntropy,
            _ if value < 0 => Error::Unknown(value),
            _ => Error::Unexpected(value as u64),
        }
    }
}

pub type Result<T> = result::Result<T, Error>;
